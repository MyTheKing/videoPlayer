import { Playlist, Track } from './types';

export const PLAYLISTS: Playlist[] = [
  {
    id: '1',
    title: '学习列表',
    trackCount: 42,
    thumbnails: [
      'https://lh3.googleusercontent.com/aida-public/AB6AXuARTqymPcaW14DLWW3pxEDkakwmglUiKVX_VdF5Ahi3pDt6UIpIuezOtOLkSLgQaoHM5xWbVesn5iYVmry3FJ4xIRRvAFiZTfiwIclhtJo5LBbEeUXpRzJRtP09lZi1AY_dKQkGwy7Zh17Kr5HlLjocf5TTD7Sc6HlZ-3CXdDqv88By_abqBqDHOMoFKTM9q8khuFRCFRHP6Dl-K03LvM56QLKwYlpWPL81KGVWEFPSYNyOqd2JOhA7hH1kM-jD5N2xCEAPpYX2kGc',
      'https://lh3.googleusercontent.com/aida-public/AB6AXuAT_e0fVcgOpzs99wIg4j6lcrlYImsupFgAJxl_NTrNtIuMal-ErFf9iUgO3QISlSRLBzuri6oU5lhIP_XLIDH_Qskn1kyg0ciJhSqbY-30w_twRD4IQnOD0zmy8QnFTvZcKSLqJHLQnasPoshO4g_OWXFOiSLFXUgaSs0iaW_2owqhkfJkqkKcIbAqXSc3z9HttbVAqU_a2_lZ2vZFD8Og6MKUpsOhd5LX4xIQDEWF4-TdFHQS9nopoxMqYLmUsG9mH1UDup25EYI',
      'https://lh3.googleusercontent.com/aida-public/AB6AXuBs70bs3PAnZrmL0CWjT5nZAw2oqnqmP3Zbz2oPRnOjwE7dfiRiLetIi1-WzRzyE_dsvHEjPKrZnzWG2peJmMkCffFFP0wFwEcxAlB_d8C3lziTkFBHJ1H5ItAYDoau2S5_YsPBOLB6yY-RtwX9fnDWPRY4J-hdN4xzw7lSORyZ2T9Tus699w-eeSxRpJiqBqkStRBmMXuJSrwtXnM1geMOw7TJrcezCXJ4_XhneCOStt0LcE_Aei3wYLaFRjWVGTa3Yyje5HxUUFE',
      'https://lh3.googleusercontent.com/aida-public/AB6AXuBzuHIuIjPmYE3O70wM4ubd9xqKEagJlpyFBogABaH3n6YyutoklAvaRJh8-dFXdLpi9JdcgK55S8fq7Qon0mcRFwlKn0Eeoq1wuHzQr-5eLB9f03o4qr3A0H9Bi3YQOQVC3orpIKGOscOYlS_F9ES5j2ZwIOtOGo5ijEm3_T5bM0E-SLHJzVwxAt896GMT30RKr1776W7W8AzYc-UlwyHtYmyk_sPi8kTZuc0fZ7icE1nsaWUbZfOqBZ9BRy-YnapNk32_dzuOSDg'
    ]
  },
  {
    id: '2',
    title: '运动混音',
    trackCount: 28,
    thumbnails: [
      'https://lh3.googleusercontent.com/aida-public/AB6AXuCq7wytq1ahdQibusOYkpJVRc-OGfZj3y4VP3cBS7G3NsZLQ3IDM4P8px1awpceKC3dEHTcaR9iDitXit0JW01yc7pZcvQmQxjYAT7K5ofFFOso6kxfD2IKmzFe7pApAm_NPABDN1orTmWyOr1X4J5_7OVuZfS3vmbsdWo95W0ZJsochyxxQm4frz226HUL8L4sOKihtTrCMre2xbN2jh6otHJklKJFqfa8eQZX8i0w7VMcTuJkpPB6eWxBD5_EUDPfoxzB12ygKls',
      'https://lh3.googleusercontent.com/aida-public/AB6AXuA1MTpBvHev3l8ERabmCdzy9w9biQ_58cKMlFEvv3xqX5j37R8ZqJLKlLXJiCjruXXal5_8qXWX9NLsYJZKI40ypBjKAnAlbPoBml9tlerxzEo6vz3Xp-6_yEpY8o7eu3Nm3Rh62z0NX3IJNzmFcZ3544_qWyEllKK-aGEA2VTCQrrmLo1Q7Jg3J7BRdC4lPTQ8ijIsQpfR6Seei7wU6A7bEisiOPmwW9OPF8NCKOjw4jNsFaRMNZN-8Jbl5geaphFF0ynxTCLXlDQ',
      'https://lh3.googleusercontent.com/aida-public/AB6AXuA7gAZZuRSWpiHZ3QOvYGJadkD73G0sYC34wPGoI_Vbg8spB4OQ6WIQNDPX82gB1tMbNVBKglNuXXVhkOmEGnPcXnSoEhLRruajj7bX9UN5pOY9mC5k0YBlMEDUSKGI8DsKL-fcJ8HxpBOlSX0dl6AwsbNO22CwHtuRXYMoytL5XhR19v9wEpx38SZGJ8SkPgPCtbO2koZHZoSpr1V7AxLY2qJ9j4b9SJ3FBMagjp_KP67R0enBaf8w4krj1jbF2FyXOLR8KYgzlYE',
      'https://lh3.googleusercontent.com/aida-public/AB6AXuBDqAcUpMtSLH_VleRVSqj6jVblS66gt-hCCs66FX7csmX7dmiyS4AHfIN-_xJe8SN7UY_YJLTtqqyUoyMKHRfk5L_MZUObV-fSUYFmTHFC4t16CcWBf4H2VT1MJ_HKoctLI_B7Wl7u3fdyk7rwfsivFNKFXSxcT_Qux0Tmsvs5az1gC3UMj1OVWY423xhTWCgeLhoU2ntQqan19OwfVyKgbFPQwOcOt4k4GKxi0PFE6ZeqGKX5Rn031T5dVkDGtBwVuKM8HansWv0'
    ]
  },
  {
    id: '3',
    title: '放松心情',
    trackCount: 115,
    thumbnails: [
      'https://lh3.googleusercontent.com/aida-public/AB6AXuA52hBYD4zstrE9pqzZC7dNLv-RfqmJ6MNUHb9qM79OrfvToa9tb8e5L2DLNx0l4d412GhiZKpUxdKrp-NyrEQtvNFiZr_BRIAXu062otpNsdYRXHMfRUqlqAsAJvD4PF4SMbWKVSwtsv5PlzHizKX3aZui5miukn8V_r5oTMFBztLE1J1-Qw6ocaS8G3cdB-ndwgEfB8Y8F9BScXpRRFOPoYpAwpKdxloEAHVaeLdNGUfRXqMrvQe2OaB9H1e3131CJqnbs7HwVZc',
      'https://lh3.googleusercontent.com/aida-public/AB6AXuCPiFdSSp8NEYpimJsiNQ8-PEJnqc7cqO6hfT3hHNbJZIuxTwTTtfTIJIF_zkyEfYBilOm64gfpiBv1F_jWXdn9VDu2nxZTvslHfSuFYGrLZj-ujxNR_7O6v0oEqFdUFDdpPL6osj6wThJXVRdMDj0m4zLTONk-WhWScgIAzb4yveNaq-pbsFlAPwqc2hj2Dp5JR8HAuljnNbek66ttaq91EkYVszqOd_mcN0JbObEKid62unEyKdUMSwc7PPcFLuMoI0zUDN_aY44',
      'https://lh3.googleusercontent.com/aida-public/AB6AXuAMIupPaisTRIHU5pq2ZPBvkj1YbG23jJTvSL5UtvyPwjXTBICJWoFdanV5Og2ga9Qaztx_8NHQNpjnFw8mTQqmbytg78BFHMSFoa-HrH8swiTwS158rMSRg9FTxZlI_7CcPkaDIV7f71xhu2eYhKkDX5sx-JGzSGFq1R9e5zXcEkSymA6a3W49KlJ8HL1jfNH7yNithEL9PY4O7hjCktJQ79Ck1sBXljaXVLUOJ5I0GYIWjni7y0fgcFAS7f9PhHsfxxV3ii1USU8',
      'https://lh3.googleusercontent.com/aida-public/AB6AXuAdTyVewiJZquKX3Fc-WncB4zYZEQGByVqocL0udRuAKz-O-SQozfR94OSl6ED6iJmerq0H4qsYUyj3v85kgka84VcbXclX1q1D1lUi1NOmt0-mdwV0eV3-Bik9OT5h89zoafDGl3crVUjMowbS4N1Uj3FovQCyTs7khbwiqOsewmvPbx22hpXtoVbEYe8wgfV-Y_5U1Eqi3BCfoWDwFxEzEoMd35myKFb1XT2bDdnJTKBYho64alb_m8aI962BH1MSdXGt-x5rmls'
    ]
  },
  {
    id: '4',
    title: '2024 公路旅行',
    trackCount: 56,
    thumbnails: [
      'https://lh3.googleusercontent.com/aida-public/AB6AXuD8ACSHWlf6JVs_p1lKkxEcITA0FfuKVV10gBLyA7xZGz_aXzzCPDQ9V1EFAG4acvzbd3WnNpFY7OkfJzO1kYh1tP9rDCdWpzP4wimvqlTzlipFm4Uu-bxzXqXQZ0q8LJYaKuLzv48vKYSkICGQ4nAIlkLm0kXlobTwDWaqtrNDJg8XNeH-avUgWLf5Wgg8XL1YU9tTCUguIsO9aj3sflEOkdhXj5qlr6auLBKasTSO3TbyZ7YB3trtc6RtemZWAKd6P8xUS9zMAzk',
      'https://lh3.googleusercontent.com/aida-public/AB6AXuBVi8l7LHRrMiXA65MJsH2LRBPI4Hi_1ueuZgTWtfyJ_OUAdnC5Q2vgAD-nnzNF5jwWMJEQzEuIWfr3OjqLWAql1lM5GKlh0Yf9wyiPcCQVRDstSAmZmrtrdkT6W4E-IIGevVY_xrv5cz4Hfd9vguV-4dt6o_q0Dj6kGyJac7CxlvVA2GSDrREzWYN8ay3nSS2CQ1RBiBrQhgcM_rWboDS7LIT79u35QPW7RTZEvcwnIxsxK3ogM8Ynm9oX5g4jlD7y0K0308cc0xc',
      'https://lh3.googleusercontent.com/aida-public/AB6AXuC312uLS6Qs94U4Y8C6So0Sbx225Lt_WipER13p1Jn-q4VwNBa01X6ynTQsrmWEYHCBHFSla-sYy30xAdVCh8Za9V8MPVAIZzFXhqdcjIVlS1WyK4TyBXJeiC0hA5PjGk14QU5EksbITZecJFPSLpmS6k6gMcgNB-0VG3o7j9NMtZHodMt42PRR8L_zo9V1A-zqkuuYE6j33BPg5X-xssHWAQEer2oK6esamjVuM31vQdDIRnFN847SN8FhS5FGXwPEB1rb1u8it20',
      'https://lh3.googleusercontent.com/aida-public/AB6AXuCkedxCPFszXO-DRaevg7NPdyfQwsV7V5ZPvklVQuDs-eRTAk7X6fHS6QlUFP7d2uc7-lC4ZQ1GizF1xs2oK6381W0jEOjA-RTQMdowGXhv7r7hIEYq9-io4QwOM1L3xF1lINZSz5GfeTt1liMCCFa4LoooihaPV5mxVFZojoSAfS3qGfwIA943IA6eRyavs7xXEXtR44z4D8iFKiq7KIZ_fiF43VWfPhr4UaPas_8Z21RB6VVIM78SdQlaE02QaKs2CUqLJ7zI3kM'
    ]
  }
];

export const TRACKS: Track[] = [
  {
    id: '101',
    title: '晨间律动电影感.mp4',
    artist: 'Aether Productions',
    duration: '04:22',
    size: '124 MB',
    date: '2023年10月12日',
    thumbnail: 'https://lh3.googleusercontent.com/aida-public/AB6AXuDrJm-EpI5YN1aOZmYjA8vSb_QBVMm3ywFsLwY8DFHbMFMKM7hedI93cTetFrnvHNXRpnjcy1ZksIMF8hjDu0JtmERsEkRU1SvA6vfh9bkCsSApgDfMAXNvlzJqgbg-vhg7q9ORrMKwUM9HNc54Rfn8Y2LpfFrmx4mzJt95HZFC3Qe79lB6S9QKTxhT71KgkBbtk5MjDP5WeytN9s1EEdm8HHNKBdwLKxyEP8px2JvNqB-404oTtJYr3NDWka4ma99eJ0spkcFmFhA'
  },
  {
    id: '102',
    title: '家庭假期 - 海滩日.mov',
    artist: 'Travel Moments',
    duration: '12:15',
    size: '856 MB',
    date: '2023年9月28日',
    thumbnail: 'https://lh3.googleusercontent.com/aida-public/AB6AXuChZVfSSyhCiKshU1RrOoSlwUt-Pa0Dx9EQa-bng2GC4IH8nDJy0J_Sh0WtEsKufPK2qzkVqzWrUH5AvHkEYLir1XlOODRs6j_ot-5vzBs51SVSKXkpHg_cDsaz_Gz301byMQUm2O9Higj5eMsDj3yWqEbNW4BExvzKAryyxwdCI9ia8YsfO-auRTIAspnmbIrjeZjk-WI5blHgkiySchSAr_oO-LUap6Y4C1zyilrm8SH7aHPhSQLASJzQwJ8CI46ycRmHgZuRpBk'
  },
  {
    id: '103',
    title: 'React Hooks 深度解析 - 教程.mp4',
    artist: 'Dev Mastery',
    duration: '08:45',
    size: '240 MB',
    date: '2023年8月15日',
    thumbnail: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBHT3cybjjAMJQ1mejxoGjaCr9zNZaygTCppTyAzPwKFYkCWuza2_RMnEgl2kar-t0VmYwgsq7w4QciePYwxTfJLuoNRATpgzh0veCdoFlLQLODHaHNtxF1CjUfERGH5hQBDRbYCiZDtA7zh4oBoo_gAyCpTrNn-PsAcVW19gdHASWVUFjAzL11iyVky1xyrvoVMhKgifypTszq57-3Fp7YRJ_Us-CA13yUI3ycwKua4ujGxsvG07_KF4eoH80VLLDq22_rZqs4vdg'
  },
  {
    id: '104',
    title: '结项演示报告.mp4',
    artist: 'Corporate Archive',
    duration: '01:30',
    size: '45 MB',
    date: '2023年6月5日',
    thumbnail: 'https://lh3.googleusercontent.com/aida-public/AB6AXuAzCPrj3ytnD6vmCvKC1gXdPa6ATcZDNxiTew0K7H8ihXgr5DiLRv1VaYS9G0QNQQ754BAjBSe_LaEiznuCt7-4IpmhAEuw0X2gadKoj3x45YJrhwjyWcwg99cY-ovs5BwfZk6JaWD-lqmH9gA6TcIaFzO3YZvYR25JZNPLWC8M-dciSOHrIAeP2m3XDl-42cQIsjUczweRIhzt8GVev0eUxop-_AjIdfjdaxGqzUZPCjUW1Qh1RXXypDACWXOwb2lcXdN0m2IfUXg'
  }
];
